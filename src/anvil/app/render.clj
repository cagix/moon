(ns anvil.app.render
  (:require [anvil.app :as app]
            [anvil.world :as world]
            [clojure.gdx.graphics.color :as color]
            [clojure.gdx.utils.screen-utils :as screen-utils]
            [gdl.stage :as stage]))

(defn-impl app/render [_]
  (screen-utils/clear color/black)
  (world/render)
  (stage/render))
