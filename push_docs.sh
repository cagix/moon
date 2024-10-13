set -ex
git checkout gh-pages
git merge main --commit
lein codox
rm -rf docs
mv target/doc docs
git add docs
git commit -m 'Update docs'
git push
git checkout main
