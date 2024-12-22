(ns anvil.app.render
  (:require [anvil.app :as app]
            [clojure.gdx.graphics.color :as color]
            [clojure.gdx.utils.screen-utils :as screen-utils]
            [gdl.stage :as stage]
            [gdl.utils :refer [defn-impl]]))

(defn world [])

(defn-impl app/render [_]
  (screen-utils/clear color/black)
  (world)
  (stage/render))
