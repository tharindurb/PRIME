# Patient Reported Information Multidimensional Exploration (PRIME)
<p align="justify">PRIME is an automated platform to investigate Online Support Group (a.k.a. health forums, online health groups) discussions for investigation of individualised patient behaviours and patient information, over time. It captures patient demographics, clinical factors, expressed emotions and decision-making behaviour based on the self-disclosed information encapsulated in the free-text posts of OSG. It further incorporates the temporality of the time sensitive events e.g., emotions, side-effects to capture the trajectories of the disease progression.</p>

<p align="justify">While PRIME can be applied to investigate any Online Support Group (OSG), its extraction of clinical factors is currently optimised for prostate cancer related OSGs. However, it can easily be adapted to investigate any other disease by altering the relevant consumer health thesauruses included in the <strong>models</strong> folder.</p>
  
<p align="justify">PRIME is composed of an ensemble of machine learning (ML) algorithms and natural language processing (NLP) techniques adapted to address the nature, content and variety of these discussions.</p>

### Source and Binaries
<p align="justify">The java source of PRIME is included in the <strong>src</strong> folder which needs to be build using JAVA 1.8 and the dependencies included in the <strong>pom.xml</strong> need to be linked using <strong>maven</strong>.</p>

<p align="justify">The executable version of PRIME is included in the <strong>bin</strong> folder with the execution instructions.</p>


### Data
<p align="justify">PRIME is trialled on a large collection of Prostate cancer related OSG discussion collected from 10 large OSGs using web scraping techniques. While the collected data is publicly available in the respective OSG websites, in order to protect the privacy of the users the dataset is not included with this open source distribution. However, the dataset will be made available to the researchers who have ethics approval and wish sign a data usage agreement.</p>

### Contributors 
<p align="justify">PRIME is a collaborative effort from the machine learning researchers of <a href="https://www.latrobe.edu.au/centre-for-data-analytics-and-cognition "><strong>Research Centre for Data Analytics and Cognition</strong></a> with the guidance  and support from the clinicians at <a href="http://www.austin.org.au"><strong>Austin Health</strong></a>.</p>

### Publications:
<p align="justify">The architecture, algorithms are explained and the results are discussed in the following publications.</p>

<ol>
<li>Bandaragoda T, Ranasinghe W, Adikari A, de Silva D, Lawrentschuk N, Alahakoon D, Persad R, Bolton D(2018) The Patient-Reported Information Multidimensional Exploration (PRIME) Framework for Investigating Emotions and Other Factors of Prostate Cancer Patients with Low Intermediate Risk Based on Online Cancer Support Group Discussions. Annals of Surgical Oncology 1–9. <a href="https://link.springer.com/article/10.1245/s10434-018-6372-2"><strong>[link]</strong></a></li>
<li>Ranasinghe W, Bandaragoda T, De Silva D, Alahakoon D (2017) A novel framework for automated, intelligent extraction and analysis of online support group discussions for cancer related outcomes. BJU International 120:59–61. <a href="https://onlinelibrary.wiley.com/doi/full/10.1111/bju.14036"><strong>[link]</strong></a></li>
<li>Bandaragoda TR, De Silva D, Alahakoon D, Ranasinghe W, Bolton D (2018) Text mining for personalised knowledge extraction from online support groups. Journal of the Association for Information Science and Technology. <strong>[accepted]</strong></li>
<li>De Silva D, Ranasinghe W, Bandaragoda T, Adikari A, Iddamalgoda L, Alahakoon D, Lawrentschuk N, Persad R, Gray R, Bolton D Machine learning to support social media empowered patients in cancer care and cancer treatment decisions. <strong>[under review]</strong></li>
</ol 
