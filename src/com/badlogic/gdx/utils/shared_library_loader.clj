(ns com.badlogic.gdx.utils.shared-library-loader
  (:import (com.badlogic.gdx.utils SharedLibraryLoader
                                   Os)))

(def ^:private os->clj
  {Os/Android :android
   Os/IOS     :ios
   Os/Linux   :linux
   Os/MacOsX  :mac
   Os/Windows :windows})

(defn operating-system []
  (os->clj SharedLibraryLoader/os))
