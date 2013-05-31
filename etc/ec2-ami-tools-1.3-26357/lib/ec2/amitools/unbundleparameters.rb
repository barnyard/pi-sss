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

class UnbundleParameters < OptionParser
  
  MANIFEST_DESCRIPTION    = "The path to the AMI manifest file."
  USER_PK_PATH_DESCRIPTION= "The path to the user's PEM encoded private key."
  SOURCE_DESCRIPTION      = 'The directory containing bundled AMI parts to unbundle. Defaults to ".".'
  DESTINATION_DESCRIPTION = 'The directory to unbundle the AMI into. Defaults to the ".".'
  DEBUG_DESCRIPTION       = "Print debug messages."
  HELP_DESCRIPTION        = "Display this help message and exit."
  VERSION_DESCRIPTION     = "Display the version and copyright notice and then exit."
  
  attr_accessor :manifest,
                :user_pk_path,
                :source,
                :destination,
                :show_help,
                :debug
  
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
  
  def initialize( argv, name )
    super( argv )
    
    self.banner = "Usage: #{name} PARAMETERS"
    
    #
    # Mandatory parameters.
    #
    separator( "" )
    separator( "MANDATORY PARAMETERS" )
    
    on( '-m', '--manifest PATH', String, MANIFEST_DESCRIPTION ) do |manifest|
      unless ( File::exist?( manifest ) and File::file?( manifest ) )
        raise Error::InvalidValue.new( "--manifest", manifest )
      end
      @manifest = manifest
    end
    
    on( '-k', '--privatekey PATH', String, USER_PK_PATH_DESCRIPTION ) do |p|
      unless File:: exist?( p ) and File::file?( p )
        raise Error::InvalidValue.new( '--privatekey', p )
      end
      @user_pk_path = p
    end
    
    #
    # Optional parameters.
    #
    self.separator( "" )
    self.separator( "OPTIONAL PARAMETERS" )
    
    on( '-s', '--source DIRECTORY', String, SOURCE_DESCRIPTION ) do |directory|
      unless File::exist?( directory ) and File::directory?( directory )
        raise Error::InvalidValue.new( '--source', directory ) 
      end
      @source = directory
    end  
    
    on( '-d', '--destination DIRECTORY', String, DESTINATION_DESCRIPTION ) do |directory|
      unless File::exist?( directory ) and File::directory?( directory )
        raise Error::InvalidValue.new( '--destination', directory ) 
      end
      @destination = directory
    end
    
    on( '--debug', DEBUG_DESCRIPTION ) do
      @debug = true
    end
    
    on( '-h', '--help', HELP_DESCRIPTION ) do
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
      raise Error::MissingMandatory.new( '--manifest' ) unless @manifest
      raise Error::MissingMandatory.new( '--privatekey' ) unless @user_pk_path
      
      #
      # Set defaults for optional parameters.
      #
      @source =  Dir::pwd() unless @source
      @destination =  Dir::pwd() unless @destination
    end
  end
end
