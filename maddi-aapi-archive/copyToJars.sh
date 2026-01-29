#
# maddi: a modification analyzer for duplication detection and immutability.
# Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
#
# This program is free software: you can redistribute it and/or modify it under the
# terms of the GNU Lesser General Public License as published by the Free Software
# Foundation, either version 3 of the License, or (at your option) any later version.
# This program is distributed in the hope that it will be useful, but WITHOUT ANY
# WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
# FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
# more details. You should have received a copy of the GNU Lesser General Public
# License along with this program.  If not, see <http://www.gnu.org/licenses/>.
#

cd src/main/resources/org/e2immu/analyzer/aapi/archive/analyzedPackageFiles/jdk/
cd openjdk-24.0.2
jar cf ../openjdk-24.0.2.jar *.json
cd ../openjdk-21.0.9/
jar cf ../openjdk-21.0.10.jar *.json
cd ../openjdk-25.0.1/
jar cf ../openjdk-25.0.2.jar *.json
cd ../../libs/
jar cf ../libs.jar */*.json

