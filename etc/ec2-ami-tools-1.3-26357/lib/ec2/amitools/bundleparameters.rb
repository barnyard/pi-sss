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
require 'timeout'
require 'ec2/platform/current'
require 'ec2/amitools/syschecks'
require 'ec2/amitools/version'

# The Bundle command line parameters.
class BundleParameters < OptionParser
  include EC2::Platform::Current::Constants

  SUPPORTED_ARCHITECTURES = ['i386', 'x86_64']
  
  USER_CERT_PATH_DESCRIPTION = "The path to the user's PEM encoded RSA public key certificate file."
  USER_PK_PATH_DESCRIPTION = "The path to the user's PEM encoded RSA private key file."
  USER_DESCRIPTION = "The user's EC2 user ID (Note: AWS account number, NOT Access Key ID)."
  HELP_DESCRIPTION = "Display this help message and exit."
  MANUAL_DESCRIPTION = "Display the user manual and exit."
  DESTINATION_DESCRIPTION = "The directory to create the bundle in. Defaults to '#{Bundling::DESTINATION}'."
  DEBUG_DESCRIPTION = "Display debug messages."
  EC2_CERT_PATH_DESCRIPTION = ['The path to the EC2 X509 public key certificate bundled into the AMI.',
                               "Defaults to '#{Bundling::EC2_X509_CERT}'."]
  ARCHITECTURE_DESCRIPTION = "Specify target architecture. One of #{SUPPORTED_ARCHITECTURES.inspect}"
  BATCH_DESCRIPTION = "Run in batch mode. No interactive prompts."
  PRODUCT_CODES_DESCRIPTION = ['Default product codes attached to the image at registration time.',
                               'Comma separated list of product codes.']
  VERSION_DESCRIPTION = "Display the version and copyright notice and then exit."

  attr_accessor :user_pk_path,
                :user_cert_path,
                :user,
                :destination,
                :ec2_cert_path,
                :debug,
                :show_help,
                :manual,
                :arch,
                :batch_mode,
                :product_codes
                
  PROMPT_TIMEOUT = 30

  class Error < RuntimeError
    class MissingMandatory < Error
      def initialize(name)
        super("missing mandatory parameter: #{name}")
      end
    end
    
    class InvalidParameterCombination < Error
      def initialize(message)
        super("invalid parameter combination: #{message}")
      end
    end
    
    class PromptTimeout < Error
      def initialize(name)
        super("Timed out waiting for user input: #{name}")
      end
    end
    
    class InvalidValue < Error
      def initialize(name, value)
        super("#{name} value invalid: #{value.to_s}")
      end
    end
    
    class InvalidExcludedDirectory < Error
      def initialize(dir)
        super("--exclude directory invalid: #{dir}")
      end
    end
  end

  def user_override(name, value)
    if interactive?
      begin
        STDOUT.print "Please specify a value for #{name} [#{value}]: "
        STDOUT.flush
        Timeout::timeout(PROMPT_TIMEOUT) do
          instr = gets
          return instr.strip unless instr.strip.empty?
        end
      rescue Timeout::Error
        raise Error::PromptTimeout.new(name)
      end
    end
    value
  end

  def notify(msg)
    STDOUT.puts msg
    if interactive?
      print "Hit enter to continue anyway or Control-C to quit."
      gets
    end
  end

  def interactive?()
    not (@batch_mode or @show_help or @manual)
  end

  def initialize( argv,
                  name,
                  add_mandatory_parameters_proc = nil,
                  add_optional_parameters_proc = nil )
    super( argv )
    
    self.banner = "Usage: #{name} PARAMETERS"
    
    #
    # Mandatory parameters.
    #
    separator( "" )
    separator( "MANDATORY PARAMETERS" )
    
    # Allow the child class to add specialised parameters.
    add_mandatory_parameters_proc.call() if add_mandatory_parameters_proc
    
    on( '-c', '--cert PATH', String, USER_CERT_PATH_DESCRIPTION ) do |p|
      unless File::exist?( p )
        raise "the specified user certificate file #{p} does not exist"
      end
      @user_cert_path = p
    end
    
    on( '-k', '--privatekey PATH', String, USER_PK_PATH_DESCRIPTION ) do |p|
      unless File::exist?( p )
        raise "the specified private key file #{p} does not exist"
      end
      @user_pk_path = p
    end
    
    on( '-u', '--user USER', String, USER_DESCRIPTION) do |p|
      # Remove hyphens from the Account ID as presented in AWS portal.
      @user = p.gsub( "-", "" )
      # Validate the account ID looks correct (users often provide us with their akid or secret key)
      unless (@user =~ /\d{12}/)
        raise "the user ID should consist of 12 digits (optionally hyphenated); this should not be your Access Key ID"
      end
    end
    
    #
    # Optional parameters.
    #
    self.separator( "" )
    self.separator( "OPTIONAL PARAMETERS" )
    
    # Allow the child class to add specialized parameters.
    add_optional_parameters_proc.call() if add_optional_parameters_proc
    
    on( '-d', '--destination PATH', String, DESTINATION_DESCRIPTION ) do |p|
      unless File::exist?( p ) and File::directory?( p )
        raise Error::InvalidValue.new( '--destination', p ) 
      end
      @destination = p
    end
    
    on( '--ec2cert PATH', String, *BundleParameters::EC2_CERT_PATH_DESCRIPTION ) do |p|
      @ec2_cert_path = p
    end
    
    on( '--debug', DEBUG_DESCRIPTION ) do
      @debug = true
    end
    
    on( '-h', '--help', HELP_DESCRIPTION ) do
      @show_help = true
    end
    
    on( '-m', '--manual', MANUAL_DESCRIPTION ) do
      @manual = true
    end
    
    on( '-r', '--arch ARCHITECTURE', String, ARCHITECTURE_DESCRIPTION ) do |a|
      @arch = a
    end
    
    on( '-b', '--batch', BATCH_DESCRIPTION ) do
      @batch_mode = true
    end

    on( '--version', VERSION_DESCRIPTION ) do
      puts version_copyright_string()
      exit
    end

    #
    # Parse the command line parameters.
    #
    parse!(argv)

    #
    # Check that we have a working tar of an appropriate version
    #
    tarcheck = SysChecks::good_tar_version?
    raise "missing or bad tar" if tarcheck.nil?
    if not tarcheck
      warn("Possibly broken tar version found. Please use tar version 1.15 or later.")
    end

    verify_mandatory_parameters

    if @arch.nil?
      @arch = SysChecks::get_system_arch()
      raise "missing or bad uname" if @arch.nil?
      @arch = user_override("arch", @arch)
    end

    if not SUPPORTED_ARCHITECTURES.include?(@arch)
      notify("Unsupported architecture [#{@arch}].")
    end

    #
    # Set defaults for optional parameters.
    #
    @destination = Bundling::DESTINATION unless @destination
    @ec2_cert_path = Bundling::EC2_X509_CERT unless @ec2_cert_path
    @exclude = [] unless @exclude
    
  end
  
  def verify_mandatory_parameters    
    unless @show_help or @manual
      raise Error::MissingMandatory.new( '--cert' ) unless @user_cert_path
      raise Error::MissingMandatory.new( '--privatekey' ) unless @user_pk_path
      raise Error::MissingMandatory.new( '--user' ) unless @user
    end
  end
end
