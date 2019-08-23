require 'fileutils'
extend FileUtils

REPO ='git@github.com:beyama/winter.git'
CNAME = "winter.jentz.io"
TMP_DIR = 'tmp_clone'
WORKING_DIR = File.dirname(File.expand_path(__FILE__))
TRY_RUN = !ARGV.empty?

cd WORKING_DIR

current_branch = `git rev-parse --abbrev-ref HEAD`
puts "Generating website based on branch #{current_branch}"

# checkout gh-pages branch into TMP_DIR
rm_rf TMP_DIR

if TRY_RUN
	mkdir TMP_DIR
else
	`git clone #{REPO} #{TMP_DIR}`

	cd TMP_DIR

	`git checkout -t origin/gh-pages`

	# Remove old site
	rm_rf '*'

	cd WORKING_DIR
end

# write CNAME file to gh-pages
File.open(File.join(TMP_DIR, "CNAME"), "w") { |f| f.write(CNAME) }

# build Javadocs
`./gradlew dokka`

# copy Javadocs
Dir["*/build/javadoc"].each do |dir|
  cp_r dir, TMP_DIR
end

# build asciidoc page
latest_version = `git describe master --abbrev=0 --tags`.strip
`asciidoctor --attribute winterVersion=#{latest_version} --destination-dir #{TMP_DIR} doc/index.adoc`

if TRY_RUN
	if RUBY_PLATFORM =~ /darwin/
		`open #{File.join(TMP_DIR, "index.html")}`
	end

	exit 0
end

cd TMP_DIR

# Stage all files in git and create a commit
`git add .`
`git add -u`
`git commit -m "Website at $(date)"`

# Push the new files up to GitHub
`git push origin gh-pages`

# Clean up
cd WORKING_DIR
rm_rf TMP_DIR
