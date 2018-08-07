del healingwell_OSG_collection.log
java -Xmx2g -cp PRIME.jar;./libs/* data_collection.HealingwellMiner >> healingwell_OSG_collection.log

del cancerforums_OSG_collection.log
java -Xmx2g -cp PRIME.jar;./libs/* data_collection.CancerforumsMiner >> cancerforums_OSG_collection.log