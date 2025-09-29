(ns com.badlogic.gdx.backends.lwjgl.files
  (:import (com.badlogic.gdx Files
                             Files$FileType)
           (com.badlogic.gdx.files FileHandle)
           (com.badlogic.gdx.utils GdxRuntimeException)
           (java.io File)))

(def external-path (str (System/getProperty "user.home") File/separator))
(def local-path    (str (.getAbsolutePath (File. ""))    File/separator))

(defn lwjgl3-file-handle [^File file ^Files$FileType type]
  (proxy [FileHandle] [file type]

    (child [^String name]
      (if (zero? (count (.getPath ^File (.file this))))
        (lwjgl3-file-handle (File. name) (.type this))
        (lwjgl3-file-handle (File. (.file this) name) (.type this))))

    (sibling [^String name]
      (if (zero? (count (.getPath ^File (.file this))))
        (throw (GdxRuntimeException. "Cannot get the sibling of the root."))
        (lwjgl3-file-handle (File. (.getParent ^File (.file this)) name) (.type this))))

    (parent []
      (let [f (.file this)
            parent (.getParentFile f)
            parent (if (nil? parent)
                     (if (= (.type this) Files$FileType/Absolute)
                       (File. "/")
                       (File. ""))
                     parent)]
        (lwjgl3-file-handle parent (.type this))))

    (file []
      (let [file (proxy-super file)
            t ^Files$FileType (.type this)]
        (cond
         (= t Files$FileType/External) (File. external-path (.getPath file))
         (= t Files$FileType/Local)    (File. local-path    (.getPath file))
         :else file)))))

(defn create []
  (reify Files
    (getFileHandle [this fileName type]
      (lwjgl3-file-handle fileName type))

    (classpath [this path]
      (lwjgl3-file-handle (File. path) Files$FileType/Classpath))

    (internal [this path]
      (lwjgl3-file-handle (File. path) Files$FileType/Internal))

    (external [this path]
      (lwjgl3-file-handle (File. path) Files$FileType/External))

    (absolute [this path]
      (lwjgl3-file-handle (File. path) Files$FileType/Absolute))

    (local [this path]
      (lwjgl3-file-handle (File. path) Files$FileType/Local))

    (getExternalStoragePath [this]
      external-path)

    (isExternalStorageAvailable [this]
      true)

    (getLocalStoragePath [this]
      local-path)

    (isLocalStorageAvailable [this]
      true)))
