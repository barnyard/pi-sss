# Copyright 2008 Amazon.com, Inc. or its affiliates.  All Rights
# Reserved.  Licensed under the Amazon Software License (the
# "License").  You may not use this file except in compliance with the
# License. A copy of the License is located at
# http://aws.amazon.com/asl or in the "license" file accompanying this
# file.  This file is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
# the License for the specific language governing permissions and
# limitations under the License.

require 'ec2/amitools/crypto'
require 'ec2/amitools/exception'
require 'ec2/amitools/deletebundleparameters'
require 'net/https'
require 'rexml/document'
require 'tempfile'
require 'uri'
require 'ec2/common/http'

NAME = 'ec2-delete-bundle'

#------------------------------------------------------------------------------#

MANUAL=<<TEXT
#{NAME} is a command line tool to delete a bundled Amazon Image from S3 storage.
An Amazon Image may be one of the following:
- Amazon Machine Image (AMI)
- Amazon Kernel Image (AKI)
- Amazon Ramdisk Image (ARI)

#{NAME} will delete a bundled AMI specified by either its manifest file or the
prefix of the bundled AMI filenames.

#{NAME} will:
- delete the manifest and parts from the s3 bucket
- remove the bucket if and only if it is empty and you request its deletion
TEXT

#------------------------------------------------------------------------------#

RETRY_WAIT_PERIOD = 5
PROMPT_TIMEOUT = 30

#------------------------------------------------------------------------------#

class DeleteFileError < RuntimeError
  def initialize( file, reason )
    super "Could not delete file '#{file}': #{reason}"
  end
end

#----------------------------------------------------------------------------#

# Delete the specified file.
def delete( s3_url, bucket, file, retry_delete, user = nil, pass = nil)
  basename = File::basename( file )
  url = "#{s3_url}/#{bucket}/#{basename}"
  loop do
    begin
      error = ''
      response = EC2::Common::HTTP::delete( url, {}, user, pass )
      break if response.success?
      error = "HTTP DELETE returned #{response.code}"
      unless retry_delete
        raise DeleteFileError.new( path, error )
      end
    rescue EC2::Common::HTTP::Error => e
      error = e.message
    end
    STDERR.puts "Error deleting #{file}: #{error}"
    STDOUT.puts "Retrying in #{RETRY_WAIT_PERIOD} seconds..."
    sleep( RETRY_WAIT_PERIOD )
  end
end

#----------------------------------------------------------------------------#

# Return a list of bundle part filenames from the manifest.
def get_part_filenames( manifest )
  parts = []
  manifest_doc = REXML::Document.new(manifest).root
  REXML::XPath.each( manifest_doc, 'image/parts/part/filename/text()' ) do |part|
    parts << part.to_s
  end
  return parts
end

#------------------------------------------------------------------------------#

def uri2string( uri )
  s = "#{uri.scheme}://#{uri.host}:#{uri.port}#{uri.path}"
  # Remove the trailing '/'.
  return ( s[s.size - 1 ] == 47 ? s.slice( 0..( s.size - 2 ) ) : s )
end

#------------------------------------------------------------------------------#

def get_file_list_from_s3( s3_url, p )
  files_to_delete = []
  response = EC2::Common::HTTP::get( "#{s3_url}/#{p.bucket}?prefix=#{p.prefix}&max-keys=1500",
                                     nil,
                                     {},
                                     p.user,
                                     p.pass )
  unless response.success?
    raise "unable to list contents of bucket #{p.bucket}: HTTP #{response.code} response: #{response.body}"
  end
  REXML::XPath.each( REXML::Document.new(response.body), "//Key/text()" ) do |entry|
    entry = entry.to_s
    files_to_delete << entry if entry =~ /^#{p.prefix}\.part\./
    files_to_delete << entry if entry =~ /^#{p.prefix}\.manifest$/
    files_to_delete << entry if entry =~ /^#{p.prefix}\.manifest\.xml$/
  end
  files_to_delete
end

#------------------------------------------------------------------------------#
  
#
# Get parameters and display help or manual if necessary.
#
def main
  begin
    p = DeleteBundleParameters.new( ARGV, NAME )
    
    if p.show_help 
      STDOUT.puts p.help
      return 0
    end
    
    if p.manual
      STDOUT.puts MANUAL
      return 0
    end
  rescue RuntimeError => e
    STDERR.puts e.message
    STDERR.puts "Try #{NAME} --help"
    return 1
  end
  
  status = 1
  
  begin
    # Get the S3 URL.
    s3_uri = URI.parse( p.url )
    s3_url = uri2string( s3_uri )
    retry_delete = p.retry
    
    files_to_delete = []
    
    if p.manifest
      # Get list of files to delete from the AMI manifest.
      xml = String.new
      manifest_path = p.manifest
      File.open( manifest_path ) { |f| xml << f.read }
      files_to_delete << File::basename(p.manifest)
      get_part_filenames( xml ).each do |part_info|
        files_to_delete << part_info
      end
    else
      files_to_delete = get_file_list_from_s3( s3_url, p )
    end
  
    if files_to_delete.empty?
      STDOUT.puts "No files to delete."
    else
      STDOUT.puts "Deleting files:"
      files_to_delete.each { |file| STDOUT.puts( '   -' + File::join( p.bucket, file )) }
      continue = p.yes
      unless continue
        begin
          STDOUT.print "Continue [y/N]: "
          STDOUT.flush
          Timeout::timeout(PROMPT_TIMEOUT) do
            continue = gets.strip =~ /^y/i
          end
        rescue Timeout::Error
          STDOUT.puts "\nNo response given, skipping the files."
          continue = false
        end
      end
      if continue
        files_to_delete.each do |file|
          delete( s3_url, p.bucket, file, retry_delete, p.user, p.pass )
          STDOUT.puts "Deleted #{File::join( p.bucket, file )}"
        end
      end
    end
    
    if p.clear
      STDOUT.puts "Attempting to delete bucket #{p.bucket}..."
      EC2::Common::HTTP::delete("#{s3_url}/#{p.bucket}", {}, p.user, p.pass)
    end
    status = 0
  rescue EC2::Common::HTTP::Error => e
    STDERR.puts e.message
    status = e.code
  rescue StandardError => e
    STDERR.puts "Error: #{e.message}."
    STDERR.puts e.backtrace if p.debug
  end
  
  if status == 0
    STDOUT.puts "#{NAME} complete."
  else
    STDOUT.puts "#{NAME} failed."
  end
  return status
end

#------------------------------------------------------------------------------#
# Script entry point. Execute only if this file is being executed.
if __FILE__ == $0
  begin
    status = main
  rescue Interrupt
    STDERR.puts "\n#{NAME} interrupted."
    status = 255
  end
  exit status
end
