(ns forge.stage
  (:require [clojure.gdx.scene2d.stage :as stage]
            [forge.app :as app]
            [forge.graphics :refer [gui-mouse-position]]))

(defn mouse-on-actor? []
  (stage/hit (app/stage) (gui-mouse-position) :touchable? true))

(defn add-actor [actor]
  (stage/add (app/stage) actor))
