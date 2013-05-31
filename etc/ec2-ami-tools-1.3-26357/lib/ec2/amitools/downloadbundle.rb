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
require 'ec2/common/http'
require 'ec2/amitools/downloadbundleparameters'
require 'ec2/amitools/exception'
require 'ec2/amitools/manifestv3'
require 'getoptlong'
require 'net/http'
require 'rexml/document'

# Download AMI downloads the specified AMI from S3.

#------------------------------------------------------------------------------#

module DownloadBundleAPI
  NAME = 'ec2-download-bundle'
  def DownloadBundleAPI::download_manifest( url, path, privatekey, user, pass, debug)
    STDOUT.puts "Downloading manifest #{url} to #{path} ..."
    EC2::Common::HTTP::get( url, path, {}, user, pass, debug )
    encrypted_manifest = File::open( path ) { |f| f.read() }
    plaintext_manifest = nil
    if (encrypted_manifest !~ /^\s*<\?/)
      STDOUT.puts "Decrypting manifest ..."
      plaintext_manifest = Crypto::decryptasym( encrypted_manifest, privatekey )
      File::open( path +'.plaintext', 'w' ) { |f| f.write( plaintext_manifest ) }
    else
      plaintext_manifest = encrypted_manifest
    end
    return plaintext_manifest
  end
  
  #----------------------------------------------------------------------------#

  def DownloadBundleAPI::download_part( url, path, user, pass, debug )
    STDOUT.puts "Downloading part #{url} to #{path} ..."
    EC2::Common::HTTP::get( url, path, {}, user, pass, debug )
  end
  
  #----------------------------------------------------------------------------#

  def DownloadBundleAPI::get_part_filenames( manifest_xml )
    manifest = ManifestV3.new( manifest_xml )
    manifest.parts.collect { |part| part.filename }.sort
  end
end

#------------------------------------------------------------------------------#

# Main method.
def main
  status = 1
  begin
    p = DownloadBundleParameters.new( ARGV )
  rescue Exception => e
    STDERR.puts e.message
    STDERR.puts "Try '#{DownloadBundleAPI::NAME} --help'"
    return status
  end
  
  if p.show_help
    STDOUT.puts p.help
    return 0
  end
  
  begin
    # Download and decrypt manifest.
    bucket_url = File::join( p.url, p.bucket )
    manifest_url = File::join( bucket_url, p.manifest )
    manifest_path = File.join( p.directory, p.manifest )
    manifest_xml = DownloadBundleAPI::download_manifest(
      manifest_url, manifest_path, 
      p.privatekey, p.user, p.pass, p.debug 
    )
    
    # Download AMI parts.
    DownloadBundleAPI::get_part_filenames( manifest_xml ).each do |filename| 
      DownloadBundleAPI::download_part( 
        File::join( bucket_url, filename ),
        File::join( p.directory, filename ),
        p.user,
        p.pass,
        p.debug
      )
      STDOUT.puts "Downloaded #{filename} from #{bucket_url}."
    end
    status = 0
  rescue ParameterError => e
    STDERR.puts e.message
    STDERR.puts p.help
  rescue EC2::Common::HTTP::Error => e
    STDERR.puts e.message
    status = e.code
  rescue StandardError => e
    STDERR.puts e.message
    STDERR.puts e.backtrace if p.debug
  end
  
  if status == 0
    STDOUT.puts 'Bundle download completed.'
  else
    STDOUT.puts 'Bundle download failed.'
  end
  
  return status
end

#------------------------------------------------------------------------------#
# Script entry point. Execute only if this file is being executed.
if __FILE__ == $0
  begin
    status = main
  rescue Interrupt
    STDERR.puts "\n#{DownloadBundleAPI::NAME} interrupted."
    status = 255
  end
  exit status
end
