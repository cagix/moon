(ns clojure.gdx.utils
  (:import (com.badlogic.gdx.utils SharedLibraryLoader
                                   Os)))

(defn operating-system []
  (let [os SharedLibraryLoader/os]
    (cond
     (= os Os/Windows) :windows
     (= os Os/Linux)  :linux
     (= os Os/MacOsX) :mac
     (= os Os/Android) :android
     (= os Os/IOS) :ios)))

(defprotocol Disposable
  (dispose [_]))

(extend-type com.badlogic.gdx.utils.Disposable
  Disposable
  (dispose [this]
    (.dispose this)))

(defn disposable? [object]
  (satisfies? Disposable object))
