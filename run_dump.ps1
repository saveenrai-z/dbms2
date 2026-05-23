$sources = Get-ChildItem -Path "backend" -Recurse -Filter "*.java" | Select-Object -ExpandProperty FullName
javac -cp "lib/mongo-java-driver-3.12.14.jar" -d bin -sourcepath backend $sources
java -cp "bin;lib/mongo-java-driver-3.12.14.jar" com.timetable.DumpDB
