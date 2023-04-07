const fs = require('fs');
const parseString = require('xml2js').parseString;

// return the version number from `pom.xml` file
function parseVersion() {
  let version = null;
  const pomXml = fs.readFileSync('../../../pom.xml', 'utf8');
  parseString(pomXml, (err, result) => {
    if (result.project.version
        && result.project.version[0]) {
      version = result.project.version[0];
    }
    if ((version === null)
        && result.project.parent
        && result.project.parent[0].version
        && result.project.parent[0].version[0]) {
      version = result.project.parent[0].version[0];
    }
  });
  if (version === null) {
    throw new Error('pom.xml is malformed. No version is defined');
  }
  return version;
}

module.exports = {
  parseVersion,
};
