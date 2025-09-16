(ns clojure.gdx.utils
  (:import (com.badlogic.gdx.utils SharedLibraryLoader
                                   Os)))

(def ^:private os->clj
  {Os/Android :android
   Os/IOS     :ios
   Os/Linux   :linux
   Os/MacOsX  :mac
   Os/Windows :windows})

(defn- operating-system []
  (os->clj SharedLibraryLoader/os))

(defn dispatch-on-os
  [ctx os->executions]
  (doseq [[f params] (os->executions (operating-system))]
    (f params))
  ctx)
