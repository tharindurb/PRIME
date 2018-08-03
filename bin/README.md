# PRIME - Executing the pipeline
<p align="justify">The main data extraction pipeline of PRIME can be executed using the PRIME.jar file provided. You need to include the following dependency jars into the <strong>libs</strong> folder before executing the pipeline. </p>
<ol>
  <li>stanford-corenlp:3.6.0</li>
  <li>stanford-corenlp:models:3.6.0</li>
</ol>

<p align="justify">The following .bat files can be used to extract information and generate csv files. Details of each .bat file are given below:</p>
<ol type="i">
  <li>analyse health forum data.bat : executes the main pipeline</li>
  <li>collect authors CSV.bat : writes the collected patient information to a csv file (one record per patient) </li>
  <li>collect authors timeline CSV.bat : writes the collected timeline information to a csv file (multiple records per patient)</li>
</ol>

<p align="justify">Please refer to the pipelineproperties file to set the location of data and to provide which options (treatment type/ emotions/ side effects etc.) need to be extracted.</p>

## Data format
<p align="justify">Data are stored in a .json format. Please refer to <strong>sample.json</strong> file to understand the structure of the data. Execution of PRIME pipeline using <strong>analyse health forum data.bat</strong> read each .json file, extract infromation and include the extreacted infromation as seperate fields into the same json file. </p>
