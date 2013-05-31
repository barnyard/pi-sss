# Copyright 2008 Amazon.com, Inc. or its affiliates.  All Rights
# Reserved.  Licensed under the Amazon Software License (the
# "License").  You may not use this file except in compliance with the
# License. A copy of the License is located at
# http://aws.amazon.com/asl or in the "license" file accompanying this
# file.  This file is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
# the License for the specific language governing permissions and
# limitations under the License.

require 'ec2/amitools/bundle'
require 'ec2/amitools/bundleimageparameters'

require 'getoptlong'

MAX_SIZE = 10 * 1024 * 1024 * 1024 # 10 GB in bytes.
NAME = 'ec2-bundle-image'

# The manual.
$manual=<<TEXT
#{NAME} is a command line tool that creates a bundled Amazon Machine \
Image (AMI) from a specified loopback filesystem image.

#{NAME} will:
- tar -S the AMI to preserve sparseness of the image file
- gzip the result
- encrypt it
- split it into parts
- generate a manifest file describing the bundled AMI

#{NAME} will bundle AMIs of up to 10GB.
TEXT

begin
  # Command line parameters.
  p = BundleImageParameters.new( ARGV, NAME )
rescue Exception => e
  STDERR.puts e.message
  STDERR.puts 'try --help' unless e.is_a? BundleParameters::Error::PromptTimeout
  exit 1
end

if p.show_help
    STDOUT.puts p.help
    exit 0
  end
  if p.manual
    STDOUT.puts $manual
    exit 0
  end

begin
  # Verify parameters.
  unless File.size( p.image_path ) <= MAX_SIZE
    raise "the specified image file #{p.image_path} is too large"
  end

  optional_args = {
    :kernel_id => p.kernel_id,
    :ramdisk_id => p.ramdisk_id,
    :product_codes => p.product_codes,
    :ancestor_ami_ids => p.ancestor_ami_ids,
    :block_device_mapping => p.block_device_mapping
  }
  STDOUT.puts 'Bundling image file...'

  Bundle.bundle_image( File::expand_path( p.image_path ),
                       p.user,
                       p.arch,
                       Bundle::ImageType::MACHINE,
                       p.destination,
                       p.user_pk_path,
                       p.user_cert_path,
                       p.ec2_cert_path,
                       p.prefix,
                       optional_args,
                       p.debug,
                       false
                     )
  
  STDOUT.puts( "#{NAME} complete." )
rescue Exception => e
  STDERR.puts "Error: #{e.message}"
  STDERR.puts e.backtrace if p.debug
  STDOUT.puts "#{NAME} failed."
  exit 1  
end
