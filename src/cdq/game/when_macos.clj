(ns cdq.game.when-macos
  (:require [cdq.game :as game])
  (:import (com.badlogic.gdx.utils SharedLibraryLoader
                                   Os)))

(defn do! [transactions]
  (when (= SharedLibraryLoader/os Os/MacOsX)
    (game/execute! transactions)))
