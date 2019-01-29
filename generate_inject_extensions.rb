require 'erb'
require 'fileutils'

TEMPLATE_FILE_NAME = "inject_extension.erb"
TEMPLATE_FILE = File.join(File.dirname(__FILE__), TEMPLATE_FILE_NAME)
TEMPLATE = ERB.new(File.read(TEMPLATE_FILE), nil, '-')

class Extension

	attr_reader :target_directory, :class_name, :file_name

	def initialize(target_package, target_directory, import, class_name)
		@target_package = target_package
		@target_directory = target_directory
		@import = import
		@class_name = class_name
		@file_name = "#{class_name}Ext.kt"
		@generator = __FILE__
		@template_file_name = TEMPLATE_FILE_NAME
	end

	def get_binding
		binding
	end

end

def write_extension(extension)
	directory = File.absolute_path(extension.target_directory)
	file = File.join(directory, extension.file_name)
	
	puts "Write template to #{file}"

	FileUtils.mkdir_p(directory)

	File.open(file, "w") do |file| 
		file << TEMPLATE.result(extension.get_binding)
	end
end

[
	Extension.new(
		"io.jentz.winter.android", 
		"winter-android/src/main/kotlin/io/jentz/winter/android", 
		"android.content.ComponentCallbacks2", 
		"ComponentCallbacks2"
	),
	Extension.new(
		"io.jentz.winter.android", 
		"winter-android/src/main/kotlin/io/jentz/winter/android", 
		"android.view.View", 
		"View"
	),
	Extension.new(
		"io.jentz.winter.aware", 
		"winter/src/main/kotlin/io/jentz/winter/aware", 
		nil, 
		"WinterAware"
	)
].each { |ext| write_extension(ext) }
