(ns com.badlogic.gdx.utils.shared-library-loader
  (:import (com.badlogic.gdx.utils Os
                                   SharedLibraryLoader)))

(def ^:private os->keyword
  {Os/Android :android
   Os/IOS     :ios
   Os/Linux   :linux
   Os/MacOsX  :mac
   Os/Windows :windows})

(defn operating-system []
  (os->keyword SharedLibraryLoader/os))
