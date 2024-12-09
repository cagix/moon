(ns forge.screens.stage
  (:require [anvil.disposable :as disposable]
            [anvil.system :as system]
            [clojure.gdx.input :as input]
            [clojure.gdx.scene2d.stage :as stage]))

(defn enter [[_ {:keys [stage sub-screen]}]]
  (input/set-processor stage)
  (system/enter sub-screen))

(defn exit [[_ {:keys [stage sub-screen]}]]
  (input/set-processor nil)
  (system/exit sub-screen))

(defn render [[_ {:keys [stage sub-screen]}]]
  (stage/act stage)
  (system/render sub-screen)
  (stage/draw stage))

(defn dispose [[_ {:keys [stage sub-screen]}]]
  (disposable/dispose stage)
  (system/dispose sub-screen))
