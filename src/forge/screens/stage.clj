(ns forge.screens.stage
  (:require [anvil.screen :as screen]
            [clojure.gdx.input :as input]
            [clojure.gdx.scene2d.stage :as stage]
            [clojure.gdx.utils.disposable :as disposable]))

(defn enter [[_ {:keys [stage sub-screen]}]]
  (input/set-processor stage)
  (screen/enter sub-screen))

(defn exit [[_ {:keys [stage sub-screen]}]]
  (input/set-processor nil)
  (screen/exit sub-screen))

(defn render [[_ {:keys [stage sub-screen]}]]
  (stage/act stage)
  (screen/render sub-screen)
  (stage/draw stage))

(defn dispose [[_ {:keys [stage sub-screen]}]]
  (disposable/dispose stage)
  (screen/dispose sub-screen))
