
# Dependencies Analyzer
DX module that provides an extensible service to analyze the missing declared dependencies
* [How to use it](#how-to-use)
    * [utils:graph-missing-dependencies](#utils-graph-missing-dependencies) 
    * [utils:print-missing-dependencies](#utils-print-missing-dependencies) 

* [How to extend it](#how-to-extend) 

## <a name="how-to-use"></a>How to use?

### Basic usage
The dependencies analyzer service is available through the [Karaf console](https://academy.jahia.com/documentation/digital-experience-manager/7.2/technical/configuration-and-fine-tuning/configuring#OSGi_SSH_Console).

Use `utils:graph-missing-dependencies` to create a graph of the missing declared dependencies

    jahia@dx()> utils:graph-missing-dependencies
    
### Commands
#### <a name="utils-graph-missing-dependencies"></a>utils:graph-missing-dependencies
Create a graph of the missing declared dependencies. The files are generated in the JCR with the path "/sites/systemsite/files/dependencies-analyzer"

**Options:**  

Name | alias | Mandatory | Value | Description
 --- | --- | :---: | :---: | ---
 -s | --skip | | | Skip modules created by Jahia
 
**Example:**

    utils:graph-missing-dependencies -s  
    
#### <a name="utils-print-missing-dependencies"></a>utils:print-missing-dependencies
Print a graph of the missing declared dependencies.

**Options:**  

Name | alias | Mandatory | Value | Description
 --- | --- | :---: | :---: | ---
 -s | --skip | | | Skip modules created by Jahia
 
**Example:**

    utils:print-missing-dependencies -s  
