# PEAP - Executing the pipeline
<p align="justify">The main data extraction pipeline of PEAP can be executed using the PEAP.jar file provided. You need to include the following dependencies before executing the pipeline. </p>
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
<p align="justify">Data are stored in a .json format. Please refer to sample.json file to understand the structure of the data.</p>
