# Copyright 2008 Amazon.com, Inc. or its affiliates.  All Rights
# Reserved.  Licensed under the Amazon Software License (the
# "License").  You may not use this file except in compliance with the
# License. A copy of the License is located at
# http://aws.amazon.com/asl or in the "license" file accompanying this
# file.  This file is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
# the License for the specific language governing permissions and
# limitations under the License.

require 'ec2/amitools/bundlemachineparameters'

# The Bundle Image command line parameters.
class BundleImageParameters < BundleMachineParameters

  IMAGE_PATH_DESCRIPTION = "The path to the file system image to bundle."
  PREFIX_DESCRIPTION = "The filename prefix for bundled AMI files. Defaults to image name."

  attr_reader :image_path
  attr_reader :prefix
                
  def initialize( argv, name )
    add_mandatory_parameters_proc = lambda do
      on( '-i', '--image PATH', String, IMAGE_PATH_DESCRIPTION ) do |p|
        unless p and File::exist?( p )
          raise "the specified image file #{p} does not exist"
        end
        @image_path = p
      end
    end
    
    add_optional_parameters_proc = lambda do
      on( '-p', '--prefix PREFIX', String, PREFIX_DESCRIPTION ) do |p|
        @prefix = p
      end
    end
    
    super(argv, name, add_mandatory_parameters_proc, add_optional_parameters_proc)
  end
end
