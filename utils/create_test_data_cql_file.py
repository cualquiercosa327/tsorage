#!/usr/bin/env python

# Create a CQL file where some dummy data and tagsets are inserted into the Cassandra database.

from enum import Enum
import itertools
from functools import reduce
from collections import OrderedDict
from datetime import datetime
import random

class Keyspace(Enum):
	RAW = 'tsorage_ts'
	OTHER = 'tsorage'

class Type(Enum):
	LONG = 'value_tlong'
	DOUBLE = 'value_tdouble'

def compute_power_set(iterable):
	# This code comes from the official Python documentation:
	# https://docs.python.org/3/library/itertools.html#itertools-recipes
	# Example:
	# >>> list(compute_power_set([1,2,3]))
	# >>> [(), (1), (2), (3), (1, 2), (1, 3), (2, 3), (1, 2, 3)]

	iterable_list = list(iterable)
	iterable_n = len(iterable_list)

	return itertools.chain.from_iterable(itertools.combinations(iterable_list, r) for r in range(iterable_n + 1))

def generate_dynamic_tagsets(raw_tagsets):
	# Generate all dynamic tagsets.
	# 'raw_tagsets' is a dictionary where each key is a tag name and each value is a possible value.
	
	# Suppose that raw_tagsets = {
	#   'quality': ['very good', 'good', 'pretty bad', 'bad'],
	#   'weather': ['sunny', 'cloudy', 'rainy'],
	#   'moon state': ['0', '0.5', '1']
	# }
	
	tag_name_value_lists = []
	for tag_name, tag_values in raw_tagsets.items():
		tag_value_list = [{tag_name: tag_value} for tag_value in tag_values]

		tag_name_value_lists.append(tag_value_list)
	# Then, tag_name_value_lists = [
	#   [{'quality': 'very good'}, {'quality': 'good'}, {'quality': 'pretty bad'}, {'quality': 'bad'}],
	#   [{'weather': 'sunny'}, {'weather': 'cloudy'}, {'weather': 'rainy'}],
	#   [{'moon state': '0'}, {'moon state': '0.5'}, {'moon state': '1'}]
	# ]

	tagsets = []
	cartesian_product_tagsets = list(itertools.product(*tag_name_value_lists))
	# Then, cartesian_product_tagsets = [
	# ({'quality': 'very good'}, {'weather': 'sunny'}, {'moon state': '0'}),
	# ({'quality': 'very good'}, {'weather': 'sunny'}, {'moon state': '0.5'}),
	# ({'quality': 'very good'}, {'weather': 'sunny'}, {'moon state': '1'}),
	# ({'quality': 'very good'}, {'weather': 'cloudy'}, {'moon state': '0'}),
	# ...
	# ({'quality': 'very good'}, {'weather': 'rainy'}, {'moon state': '1'}),
	# ({'quality': 'good'}, {'weather': 'sunny'}, {'moon state': '0'}),
	# ...
	# {'weather': 'rainy'}, {'moon state': '1'})
	# ]
	for tag_list in cartesian_product_tagsets:
		for tag_list_power_set in compute_power_set(tag_list): # Compute all subsets of 'tag_list'.
			try:
				# Suppose that tag_list_power_set = ({'quality': 'very good'}, {'weather': 'sunny'},
				# {'moon state': '0'})
				tagset = reduce(lambda a, b: dict(a, **b), tag_list_power_set)
				# Then, tagset = {'quality': 'very good', 'weather': 'sunny', 'moon state': '0'}
			except TypeError:
				tagset = {} # Empty dictionary
			tagsets.append(tagset)

	# Remove duplicates from 'tagsets'.
	# Deterministic order:
	tagsets = list(OrderedDict((frozenset(tagset.items()), tagset) for tagset in tagsets).values())
	# Non-deterministic order:
	# tagsets = [
	# 	dict(t) for t in
	# 	{tuple(tagset.items()) for tagset in tagsets}
	# ]
	
	return tagsets

def generate_observations(sensor, num_observations, start_timestamp_ms, time_step_ms):
	observations = []
	current_timestamp_ms = start_timestamp_ms
	for _ in range(num_observations):
		# Generate a random value.
		random_value = None
		if sensor.value_type == Type.LONG:
			value = random.randint(sensor.value_min, sensor. value_max)
		elif sensor.value_type == Type.DOUBLE:
			value = round(random.uniform(sensor.value_min, sensor. value_max), 2)

		# Randomly pick a dynamic tagset.
		tagset = random.choice(sensor.dynamic_tagsets)

		observations.append(Observation(current_timestamp_ms, value, tagset=tagset))

		current_timestamp_ms += time_step_ms

	return observations

class Observation:
	def __init__(self, timestamp_ms, value, tagset=None):
		"""
		Create a Observation object.

		:timestamp_ms: The timestamp of the observation in milliseconds.
		:value: The value of the observation.
		:tagset: A dynamic tagset attached to the observation.
		"""
		self.timestamp_ms = timestamp_ms
		self.datetime = datetime.fromtimestamp(timestamp_ms / 1000)
		self.shard = self._compute_shard(self.datetime)
		self.value = value
		self.tagset = tagset

	def _compute_shard(self, datetime):
		return '{}-{}'.format(datetime.year, datetime.month)

	def __str__(self):
		return '{}: {}'.format(self.timestamp_ms, self.value)

class Sensor:
	def __init__(self, name, value_min, value_max, value_type, observations=[], static_tagsets=[], dynamic_tagsets=[]):
		"""
		Create a Sensor object.

		:name: The name of the sensor.
		:value_min: The smallest value the sensor "measure".
		:value_max: The highest value the sensor "measure".
		:value_type: The type of values (Type enumeration).
		:observations: A list of observation (Observation object).
		:static_tagsets: A list of static tagsets. Each one is a tuple of two elements: the first one is the name of a
			tagset and the second one the value of this tagset.
		:dynamic_tagsets: A list of dynamic tagsets. Each one is a dictionary where each key is a tagset name and each
			value is the corresponding value of this tagset.
		"""
		self.name = name
		self.value_min = value_min
		self.value_max = value_max
		self.value_type = value_type

		self.observations = observations

		self.static_tagsets = static_tagsets
		self.dynamic_tagsets = dynamic_tagsets

	def static_tag_to_cql_request(self, tag_name, tag_value):
		"""
		Convert a single static tag to CQL request (in string).
		"""
		
		return 'INSERT INTO {}.static_tagset (metric, tagname, tagvalue) VALUES (\'{}\', \'{}\', \'{}\');'.format(
			Keyspace.OTHER.value, sensor.name, tag_name, tag_value
		)

	def dynamic_tag_to_cql_request(self, tagset):
		"""
		Convert a single dynamic tag to CQL request (in string).
		"""
		
		return 'INSERT INTO {}.dynamic_tagset (metric, tagset) VALUES (\'{}\', {});'.format(
			Keyspace.OTHER.value, sensor.name, tagset
		)

	def observation_to_cql_request(self, observation):
		"""
		Convert a single observation to CQL request (in string).
		"""
		
		observation_str = 'INSERT INTO {}.observations (metric, tagset, shard, datetime, {}) VALUES ' \
			'(\'{}\', {}, \'{}\', {}, {{value: {}}});'
		observation_str = observation_str.format(Keyspace.RAW.value, self.value_type.value, sensor.name,
			observation.tagset, observation.shard, observation.timestamp_ms, observation.value)

		return observation_str

	def __str__(self):
		return '{}'.format(self.name)

if __name__ == '__main__':
	random.seed(42)

	cql_file_name = 'test_data.cql'

	# Arguments for the generation of the observations.
	num_observations = 100
	start_timestamp_ms = 1575136800000
	time_step_ms = 300000 # Corresponds to 5 minutes.

	# Create sensors.
	temperature_sensor = Sensor('temperature', -5, 30, Type.DOUBLE,
		static_tagsets=[('unit', 'degrees celsius')],
		dynamic_tagsets=generate_dynamic_tagsets({'quality': ['very good', 'good', 'pretty bad', 'bad'],
			'weather': ['sunny', 'cloudy', 'rainy']})
	)
	humidity_sensor = Sensor('humidity', 0, 100, Type.LONG,
		static_tagsets=[('unit', 'humdity percentage')],
		dynamic_tagsets=generate_dynamic_tagsets({'quality': ['very good', 'good', 'pretty bad', 'bad'],
			'weather': ['sunny', 'cloudy', 'rainy']})
	)
	pressure_sensor = Sensor('pressure', 1015, 1030, Type.DOUBLE,
		static_tagsets=[('unit', 'hectopascal')],
		dynamic_tagsets=generate_dynamic_tagsets({'weather': ['sunny', 'cloudy', 'rainy']})
	)
	sensors = [temperature_sensor, humidity_sensor, pressure_sensor]

	# Generate observations.
	for sensor in sensors:
		sensor.observations = generate_observations(sensor, num_observations, start_timestamp_ms, time_step_ms)

	## Create CQL file.
	cql_file_lines = []

	# Create static tagset lines.
	cql_file_lines.append('// Insert static tagsets.')
	for sensor in sensors:
		for tag_name, tag_value in sensor.static_tagsets:
			tag_line = sensor.static_tag_to_cql_request(tag_name, tag_value)
			cql_file_lines.append(tag_line)
	cql_file_lines.append('')

	# Create dynamic tagset lines.
	cql_file_lines.append('// Insert dynamic tagsets.')
	for sensor in sensors:
		for tagset in sensor.dynamic_tagsets:
			tag_line = sensor.dynamic_tag_to_cql_request(tagset)
			cql_file_lines.append(tag_line)
	cql_file_lines.append('')

	# Create observation lines.
	cql_file_lines.append('// Insert observations.')
	for sensor in sensors:
		for observation in sensor.observations:
			tag_line = sensor.observation_to_cql_request(observation)
			cql_file_lines.append(tag_line)

	# Write lines into a CQL file.
	with open(cql_file_name, 'w') as file:
		for line in cql_file_lines:
			file.write('{}\n'.format(line))
