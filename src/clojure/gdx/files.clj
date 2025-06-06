(ns clojure.gdx.files
  (:import (com.badlogic.gdx Files)))

(defn internal [^Files files path]
  (.internal files path)

  ; =>
  ;(Lwjgl3FileHandle path FileType/Internal)

  ; =>
  ;(FileHandle. path FileType/Internal)

  ; =>
	;protected FileHandle (String fileName, FileType type) {
  ;	this.type = type;
  ; file = new File(fileName);
  )
