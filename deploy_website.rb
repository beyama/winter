#!/usr/bin/env ruby

require 'fileutils'
extend FileUtils

REPO='git@github.com:beyama/winter.git'
GROUP_ID = 'io.jentz.winter'
DIR = 'tmp_clone'

rm_rf DIR
`git clone #{REPO} #{DIR}`

cd DIR

`git checkout -t origin/gh-pages`

# Remove old site
rm_rf '*'

# Download the latest javadoc
['winter', 'winter-android', 'winter-compiler', 'winter-junit4', 'winter-rxjava2'].each do |id|
    `curl -L "http://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=#{GROUP_ID}&a=#{id}&v=LATEST&c=javadoc" > javadoc.zip`
    mkdir 'javadoc' unless File.directory? 'javadoc'
    `unzip -o javadoc.zip -d javadoc`
    rm 'javadoc.zip'
end

# Checkout doc folder and convert docs to HTML
`git checkout master -- doc/*`
latest_version = `git describe master --abbrev=0 --tags`.strip
`asciidoctor --attribute winterVersion=#{latest_version} --destination-dir . doc/index.adoc`
rm_rf 'doc'


# Stage all files in git and create a commit
`git add .`
`git add -u`
`git commit -m "Website at $(date)"`

# Push the new files up to GitHub
`git push origin gh-pages`

# Clean up
cd '..'
rm_rf DIR