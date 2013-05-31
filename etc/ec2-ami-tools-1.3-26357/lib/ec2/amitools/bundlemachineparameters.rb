# Copyright 2008 Amazon.com, Inc. or its affiliates.  All Rights
# Reserved.  Licensed under the Amazon Software License (the
# "License").  You may not use this file except in compliance with the
# License. A copy of the License is located at
# http://aws.amazon.com/asl or in the "license" file accompanying this
# file.  This file is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
# the License for the specific language governing permissions and
# limitations under the License.

require 'ec2/amitools/bundleparameters'

# The Bundle command line parameters.
class BundleMachineParameters < BundleParameters

  KERNEL_DESCRIPTION            = "Id of the default kernel to launch the AMI with."
  RAMDISK_DESCRIPTION           = "Id of the default ramdisk to launch the AMI with."
  ANCESTOR_AMI_IDS_DESCRIPTION  = "Lineage of this image. Comma separated list of AMI ids."
  BDM_DESCRIPTION               = ['Default block-device-mapping scheme to launch the AMI with. This scheme',
                                   'defines how block devices may be exposed to an EC2 instance of this AMI',
                                   'if the instance-type of the instance is entitled to the specified device.',
                                   'The scheme is a comma-separated list of key=value pairs, where each key',
                                   'is a "virtual-name" and each value, the corresponding native device name',
                                   'desired. Possible virtual-names are:',
                                   ' - "ami": denotes the root file system device, as seen by the instance.',
                                   ' - "root": denotes the root file system device, as seen by the kernel.',
                                   ' - "swap": denotes the swap device, if present.',
                                   ' - "ephemeralN": denotes Nth ephemeral store; N is a non-negative integer.',
                                   'Note that the contents of the AMI form the root file system. Samples of',
                                   'block-device-mappings are:',
                                   ' - "ami=sda1,root=/dev/sda1,ephemeral0=sda2,swap=sda3"',
                                   ' - "ami=0,root=/dev/dsk/c0d0s0,ephemeral0=1"'
                                  ]

  attr_accessor :kernel_id
  attr_accessor :ramdisk_id
  attr_accessor :ancestor_ami_ids
  attr_accessor :block_device_mapping

  def initialize(argv, name, add_mandatory_parameters_proc = nil, add_optional_parameters_proc = nil )
    add_optional_params = lambda do
      
      add_optional_parameters_proc.call      
      on( '--kernel ID', KERNEL_DESCRIPTION ) do |p|
        @kernel_id = p
      end
      
      on( '--ramdisk ID', RAMDISK_DESCRIPTION ) do |p|
        @ramdisk_id = p
      end
      
      on( '-B', '--block-device-mapping MAPS', String, 
          *BundleMachineParameters::BDM_DESCRIPTION ) do |p|
        @block_device_mapping ||= {}
        raise Error::InvalidValue.new( 'block-device-mapping', p ) if p.to_s.empty?
        p.split(',').each do |mapping|
          raise Error::InvalidValue.new( 'block-device-mapping', mapping ) unless mapping =~ /^\s*(\S)+\s*=\s*(\S)+\s*$/
          virtual, device = mapping.split(/=/)
          @block_device_mapping[virtual.strip] = device.strip
        end
      end
      separator('')
    end

    super(argv, name, add_mandatory_parameters_proc, add_optional_params)
  end
end
