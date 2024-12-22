(ns anvil.app.render
  (:require [anvil.app :as app]
            [anvil.lifecycle.render :refer [render-world]]
            [clojure.gdx.graphics.color :as color]
            [clojure.gdx.utils.screen-utils :as screen-utils]
            [gdl.stage :as stage]
            [gdl.utils :refer [defn-impl]]))

(defn-impl app/render [_]
  (screen-utils/clear color/black)
  (render-world)
  (stage/render))
