(ns forge.screens.stage
  (:require [clojure.component :as component]
            [clojure.gdx.input :as input]
            [clojure.gdx.scene2d.stage :as stage]
            [clojure.gdx.utils.disposable :as disposable]))

(defn enter [[_ {:keys [stage sub-screen]}]]
  (input/set-processor stage)
  (component/enter sub-screen))

(defn exit [[_ {:keys [stage sub-screen]}]]
  (input/set-processor nil)
  (component/exit sub-screen))

(defn render [[_ {:keys [stage sub-screen]}]]
  (stage/act stage)
  (component/render sub-screen)
  (stage/draw stage))

(defn dispose [[_ {:keys [stage sub-screen]}]]
  (disposable/dispose stage)
  (component/dispose sub-screen))
