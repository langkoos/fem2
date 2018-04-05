#### Workspace MATSim integration

The integration FEM2.0 product will be an integrated package combining [CSIRO Workspace](https://research.csiro.au/workspace/) with [MATSim](matsim.org). The test environment for this integration is a  Windows 10 (x64) virtual machine. The following instructions will help you set this up on your working computer.

1. [Download and install VirtualBox 5.2](https://www.virtualbox.org/wiki/Downloads) for the operating system of your computer.

1. Launch VirtualBox and import the virtual machine appliance from ```
./integration/virtualbox-vm/workspace-matsim-test-env-win10.ova```
For more information see [Importing an Existing Virtual Machine into VirtualBox](https://docs.oracle.com/cd/E26217_01/E26796/html/qs-import-vm.html).

1. Start the newly imported virtual machine, and after the startup sequence you should get to the Windows 10 login screen. The login details are
```
User: IEUser
Password: Passw0rd!
```
You should now see the following Windows 10 desktop:
![Windows 10 Virtual Machine Desktop](./doc/win10desktop.png)

1. Mount the Git repository directory so that you can access the files from within the VM. To do this follow the instructions [on this page](https://helpdeskgeek.com/virtualization/virtualbox-share-folder-host-guest/). Here we will assume that you have mapped the repository to windows drive `E:`.

1. ** All the steps so far are one-off steps. The steps from hereon is what you are likely to want to repeat everytime you want to test the latest versions of the software. At this point, you may want to create a snapshot of the VM that you can come back to each time you want to do that.**

1. Now, ensure that the model is built. You might have already done that on your host machine, but if not, do that now.
*Note that you can build from inside the VM too if you wish. To do that open the **Cygwin64 Terminal** (from the VM desktop), and do:
```
cd /cygdrive/e; mvn clean install
```
(**TODO: automate this  step**)

1. Finally, to run the automated integration test, open the **Powershell as Admin** terminal from the VM desktop and issue the following command:
```
net use E: \\VBOXSVR\au-flood-evacuation; e:\integration\run.ps1
```
 The above will do the following:
   * Map the repository to `E:`; even though we mapped this earlier already, this is required here again for the Admin terminal which does not inherit the user mapped drives.
   * Uninstall CSIRO Workspace if already installed
   * Install CSIRO Workspace from `./integration/workspace-drops/csiro.au-workspace*.exe`
   * Run the workflow from `./integration/RunMatsim4FloodEvacuationTest.wsx`
   * *Still to fix issues with windows paths; runs but fails; DS 6/apr/18]**
