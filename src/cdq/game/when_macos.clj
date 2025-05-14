(ns cdq.game.when-macos
  (:require [cdq.utils :as utils])
  (:import (com.badlogic.gdx.utils SharedLibraryLoader
                                   Os)))

(defn do! [transactions]
  (when (= SharedLibraryLoader/os Os/MacOsX)
    (utils/execute! transactions)))
