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
require 'ec2/amitools/version'

#------------------------------------------------------------------------------#

class DownloadBundleParameters < OptionParser

  BUCKET      = "The bucket to download the bundle from."
  PREFIX      = "The filename prefix for bundled AMI files. Defaults to 'image'."
  USER        = "The user's AWS access key ID."  
  PASS        = "The user's AWS secret access key."
  PRIVATE_KEY = 'The private key to decrypt the manifest with.'  
  DEBUG       = 'Print debug messages.'
  HELP        = 'Display the help message and exit.'
  URL         = "The S3 service URL. Defaults to https://s3.amazonaws.com."
  DIRECTORY   = ['The directory into which to download the bundled AMI parts.',
                 "Defaults to the current working directory."]
  MANIFEST    = ["The local manifest filename. Required only for manifests that",
                 "pre-date the version 3 manifest file format."]
  VERSION_DESCRIPTION = "Display the version and copyright notice and then exit."
  
  attr_accessor :bucket,
                :manifest,
                :prefix,
                :user,
                :pass,
                :privatekey,
                :directory,
                :show_help,
                :url,
                :debug
    
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
 
    class Conflict < Error
      def initialize( *names )
        super( "conflicting parameters were specified: #{names.join(', ')}" )
      end
    end
    
  end
  
  #----------------------------------------------------------------------------#
  
  def initialize( argv )
    super
    
    self.banner = "Usage: downloadbundles PARAMETERS"
    
    @debug = false
    
    #
    # Mandatory parameters.
    #
    separator( "" )
    separator( "MANDATORY PARAMETERS" )
    
    on( '-b', '--bucket BUCKET', String, BUCKET ) do |bucket|
      @bucket = bucket
    end
    
    on( '-a', '--access-key USER', String, USER ) do |user|
      @user = user
    end
    
    on( '-s', '--secret-key PASSWORD', String, PASS ) do |pass|
      @pass = pass
    end
    
    on( '-k', '--privatekey KEY', String, PRIVATE_KEY ) do |privatekey|
      raise Error::InvalidValue.new( "--privatekey", privatekey ) unless File::exist?( privatekey )
      @privatekey = privatekey
    end
    
    #
    # Optional parameters.
    #
    self.separator( "" )
    self.separator( "OPTIONAL PARAMETERS" )
    
    on( '-m', '--manifest FILE', String, *DownloadBundleParameters::MANIFEST ) do |manifest|
      @manifest = manifest
    end
    
    on( '-p', '--prefix PREFIX', String, PREFIX ) do |prefix|
      @prefix = prefix
    end
    
    on( '-d', '--directory DIRECTORY', String, *DownloadBundleParameters::DIRECTORY ) do |directory|
      unless File::exist?( directory ) and File::directory?( directory )
        raise Error::InvalidValue.new( '--directory', directory ) 
      end
      @directory = directory
    end
    
    on( '--url URL', String, URL ) do |url|
      @url = url
    end
    
    on( '--debug', DEBUG ) do |o|
      @debug = o
    end
    
    on( '-h', '--help', HELP ) do
      @show_help = true
    end
    
    on( '--version', VERSION_DESCRIPTION ) do
      puts version_copyright_string()
      exit
    end

    #
    # Parse the command line parameters.
    #
    parse!( argv )
    
    unless @show_help
      #
      # Verify mandatory parameters.
      #
      raise Error::MissingMandatory.new( '--bucket' ) unless @bucket
      raise Error::MissingMandatory.new( '--access-key' ) unless @user
      raise Error::MissingMandatory.new( '--secret-key' ) unless @pass
      raise Error::MissingMandatory.new( '--privatekey' ) unless @privatekey

      raise Error::Conflict.new( '--prefix', '--manifest' ) if (@prefix and @manifest)

      #
      # Set defaults for optional parameters.
      #
      @directory = Dir::pwd() unless @directory
      @url = 'https://s3.amazonaws.com' unless @url
      @prefix = @manifest.split('.')[0..-2].join('.') if (@manifest)
      @prefix = 'image' unless @prefix
      @manifest = "#{@prefix}.manifest.xml" unless @manifest
    end
  end
end
