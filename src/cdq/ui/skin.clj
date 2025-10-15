(ns cdq.ui.skin
  (:require [clojure.gdx.graphics.g2d.bitmap-font :as bitmap-font]
            [clojure.gdx.graphics.g2d.bitmap-font.data :as bmfont-data]
            [clojure.gdx.scene2d.ui.skin :as skin]
            [clojure.vis-ui :as vis-ui]
            [clojure.vis-ui.tooltip :as tooltip]))

(defn load! [{:keys [skin-scale]}]
  ; app crashes during startup before vis-ui/dispose!
  ; and we do clojure.tools.namespace.refresh -> gui elements not showing.
  (when (vis-ui/loaded?)
    (vis-ui/dispose!))
  (vis-ui/load! skin-scale)
  (-> (vis-ui/skin)
      (skin/font "default-font")
      bitmap-font/data
      (bmfont-data/set-enable-markup! true))
  (tooltip/set-default-appear-delay-time! 0))

(defn dispose! []
  (vis-ui/dispose!))
