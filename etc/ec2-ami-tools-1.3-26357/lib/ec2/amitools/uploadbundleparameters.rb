# Copyright 2008 Amazon.com, Inc. or its affiliates.  All Rights
# Reserved.  Licensed under the Amazon Software License (the
# "License").  You may not use this file except in compliance with the
# License. A copy of the License is located at
# http://aws.amazon.com/asl or in the "license" file accompanying this
# file.  This file is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
# the License for the specific language governing permissions and
# limitations under the License.

require 'optparse'
require 'ec2/platform/current'
require 'ec2/amitools/version'

#------------------------------------------------------------------------------#

class UploadBundleParameters < OptionParser
  include EC2::Platform::Current::Constants

  BUCKET_DESCRIPTION = "The bucket to upload the bundle to. Created if nonexistent."
  MANIFEST_DESCRIPTION = "The path to the manifest file."  
  USER_DESCRIPTION = "The user's AWS access key ID."
  PASS_DESCRIPTION = "The user's AWS secret access key."
  ACL_DESCRIPTION = ["The access control list policy [\"public-read\" | \"aws-exec-read\"].",
                         "Defaults to \"aws-exec-read\"."]  
  EC2_CERT_DESCRIPTION= ["The path to the EC2 X509 public key certificate for the upload.", 
                         "Defaults to \"#{Bundling::EC2_X509_CERT}\"."]
  DEBUG_DESCRIPTION = "Print debug messages."  
  DIRECTORY_DESCRIPTION = ["The directory containing the bundled AMI parts to upload.",
                      "Defaults to the directory containing the manifest."]  
  HELP_DESCRIPTION = "Display the help message and exit."  
  MANUAL_DESCRIPTION = "Display the manual and exit."  
  PART_DESCRIPTION = "Upload the specified part and upload all subsequent parts."
  RETRY_DESCRIPTION = "Automatically retry failed uploads. Use with caution."  
  SKIP_MANIFEST_DESCRIPTION = "Do not upload the manifest."  
  URL_DESCRIPTION = "The S3 service URL. Defaults to https://s3.amazonaws.com."
  VERSION_DESCRIPTION = "Display the version and copyright notice and then exit."
  
  attr_accessor :bucket,
                :manifest,
                :user,
                :pass,
                :acl,
                :ec2certificate,
                :debug,
                :directory,
                :show_help,
                :manual,
                :part,
                :retry,
                :skipmanifest,
                :url
    
  #----------------------------------------------------------------------------#
  
  class Error < RuntimeError
    
    class MissingMandatory < Error
      def initialize( name )
        super( "missing mandatory parameter: #{name}" )
      end
    end
    
    class InvalidValue < Error
      def initialize( name, value )
        super( "#{name} value invalid: #{value.to_s}" )
      end
    end
 
  end
  
  #----------------------------------------------------------------------------#
  
  def initialize( argv, name )
    super( argv )
    
    self.banner = "Usage: #{name} PARAMETERS"
    
    #
    # Mandatory parameters.
    #
    separator( "" )
    separator( "MANDATORY PARAMETERS" )
    
    on( '-b', '--bucket BUCKET', String, BUCKET_DESCRIPTION ) do |bucket|
      @bucket = bucket
    end
    
    on( '-m', '--manifest PATH', String, MANIFEST_DESCRIPTION ) do |manifest|
      raise Error::InvalidValue.new( "--manifest", manifest ) unless ( File::exist?( manifest ) and File::file?( manifest ) )
      @manifest = manifest
    end
    
    on( '-a', '--access-key USER', String, USER_DESCRIPTION ) do |user|
      @user = user
    end
    
    on( '-s', '--secret-key PASSWORD', String, PASS_DESCRIPTION ) do |pass|
      @pass = pass
    end
    
    #
    # Optional parameters.
    #
    self.separator( "" )
    self.separator( "OPTIONAL PARAMETERS" )
    
    on( '--acl ACL', String, 
        *UploadBundleParameters::ACL_DESCRIPTION ) do |acl|
      raise Error::InvalidValue.new( '--acl', acl ) unless ['public-read', 'aws-exec-read'].include?( acl )
      @acl = acl
    end
    
    on( '--ec2cert PATH', String, 
        *UploadBundleParameters::EC2_CERT_DESCRIPTION ) do |ec2certificate|
      unless File::exist?( ec2certificate ) and File::file?( ec2certificate )
        raise Error::InvalidValue.new( '--ec2cert', ec2certificate ) 
      end
      puts "Are you sure you want to specify a different EC2 public certificate? [y/n]"
      while response = $stdin.readline.chomp and ( response != 'y' and response != 'n')
        puts response
        puts "Please specify [y/n]" 
      end
      if response == 'y'
        puts "Using specified EC2 public certificate: #{ec2certificate}."
        @ec2certificate = ec2certificate
      else
        puts "Using default EC2 public certificate."
      end
      
    end
    
    on( '-d', '--directory DIRECTORY', String, 
        *UploadBundleParameters::DIRECTORY_DESCRIPTION ) do |directory|
      unless File::exist?( directory ) and File::directory?( directory )
        raise Error::InvalidValue.new( '--directory', directory ) 
      end
      @directory = directory
    end
    
    on( '--debug', DEBUG_DESCRIPTION ) do
      @debug = true
    end
    
    on( '--part PART', Integer, PART_DESCRIPTION ) do |part|
      @part = part
    end
    
    on( '--url URL', String, URL_DESCRIPTION ) do |url|
      @url = url
    end
    
    on( '--retry', RETRY_DESCRIPTION ) do
      @retry = true
    end
    
    on( '--skipmanifest', SKIP_MANIFEST_DESCRIPTION ) do
      @skipmanifest = true
    end
    
    on( '-h', '--help', HELP_DESCRIPTION ) do
      @show_help = true
    end
    
    on( '--manual', MANUAL_DESCRIPTION ) do
      @manual = true
    end
    
    on_tail( '--version', VERSION_DESCRIPTION ) do
      puts version_copyright_string()
      exit
    end

    #
    # Parse the command line parameters.
    #
    parse!( argv )
    
    
    unless @show_help or @manual
      #
      # Verify mandatory parameters.
      #
      raise Error::MissingMandatory.new( '--bucket' ) unless @bucket
      raise Error::MissingMandatory.new( '--manifest' ) unless @manifest
      raise Error::MissingMandatory.new( '--access-key' ) unless @user
      raise Error::MissingMandatory.new( '--secret-key' ) unless @pass
      
      #
      # Set defaults for optional parameters.
      #
      @acl = 'aws-exec-read' unless @acl
      @ec2certificate = Bundling::EC2_X509_CERT unless @ec2certificate
      @directory = File::dirname( @manifest ) unless @directory
      @debug = false unless @debug
      @part = nil unless @part
      @retry = false unless @retry
      @skipmanifest = false unless @skipmanifest
      @url = 'https://s3.amazonaws.com' unless @url
    end
  end
end
