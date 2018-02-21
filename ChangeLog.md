# CHANGELOG

## 0.1.2

Additional features:

 * Select and use IDEA inspection profile via `inheritFromIdea profileName="..."` in configuration file
 * Multi-process mode support (two or more plugin tasks can be launched simultaneously)
 
Bug fixes:

 * Several fixes in inspection level & log level calculation 

## 0.1.1 (first version)

This alpha-version provides basic functionality:

 * Application of `inspections` plugin
 * Select IDEA version via `toolVersion`
 * Inspection configuration via `config/inspections/inspections.xml`
 * Set of additional options (`maxErrors`, `maxWarnings`, `ignoreFailures`, `quiet`, `config`)
 * Output either to console or XML file
 * Single-process mode (simultaneous work of two or more plugin tasks is not guaranteed)