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

class DeleteBundleParameters < OptionParser
    
  BUCKET_DESCRIPTION = "The bucket containing the bundled AMI."  
  MANIFEST_DESCRIPTION = "The path to the unencrypted manifest file."  
  PREFIX_DESCRIPTION = "The bundled AMI part filename prefix."  
  USER_DESCRIPTION = "The user's AWS access key ID."  
  PASS_DESCRIPTION = "The user's AWS secret access key."  
  DEBUG_DESCRIPTION = "Print debug messages."  
  HELP_DESCRIPTION = "Display the help message and exit."  
  MANUAL_DESCRIPTION = "Display the manual and exit."  
  RETRY_DESCRIPTION = "Automatically retry failed deletes. Use with caution."  
  URL_DESCRIPTION = "The S3 service URL. Defaults to https://s3.amazonaws.com."  
  YES_DESCRIPTION = "Automatically answer 'y' without asking."
  CLEAR_DESCRIPTION = "Delete the bucket if empty. Not done by default"
  VERSION_DESCRIPTION = "Display the version and copyright notice and then exit."
  
  attr_accessor :bucket,
                :manifest,
                :prefix,
                :user,
                :pass,
                :debug,
                :show_help,
                :manual,
                :debug,
                :retry,
                :url,
                :yes,
                :clear
    
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
    
    @clear = false
    
    self.banner = "Usage: #{name} PARAMETERS"
    
    #
    # Mandatory parameters.
    #
    separator( "" )
    separator( "MANDATORY PARAMETERS" )
    
    on( '-b', '--bucket BUCKET', String, BUCKET_DESCRIPTION ) do |bucket|
      @bucket = bucket
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
    
    on( '-m', '--manifest PATH', String, MANIFEST_DESCRIPTION ) do |manifest|
      raise Error::InvalidValue.new( "--manifest", manifest ) unless ( File::exist?( manifest ) and File::file?( manifest ) )
      @manifest = manifest
    end
    
    on( '-p', '--prefix PREFIX', String, PREFIX_DESCRIPTION ) do |prefix|
      @prefix = prefix
    end
    
    on( '--clear', CLEAR_DESCRIPTION ) do
      @clear = true
    end
    
    on( '--debug', DEBUG_DESCRIPTION ) do
      @debug = true
    end
    
    on( '--retry', RETRY_DESCRIPTION ) do
      @retry = true
    end
    
    on( '--url URL', String, URL_DESCRIPTION ) do |url|
      @url = url
    end
    
    on( '-h', '--help', HELP_DESCRIPTION ) do
      @show_help = true
    end
    
    on( '--manual', MANUAL_DESCRIPTION ) do
      @manual = true
    end

    on( '-y', '--yes', YES_DESCRIPTION ) do
      @yes = true
    end
    
    on( '--version', VERSION_DESCRIPTION ) do
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
      raise Error::MissingMandatory.new( '--manifest or --prefix' ) unless @manifest or @prefix
      raise Error::MissingMandatory.new( '--access-key' ) unless @user
      raise Error::MissingMandatory.new( '--secret-key' ) unless @pass
      
      #
      # Set defaults for optional parameters.
      #
      @url = 'https://s3.amazonaws.com' unless @url
    end
  end
end
