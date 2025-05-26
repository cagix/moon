(ns cdq.create.ui-viewport
  (:require [gdl.application]
            [gdl.c :as c]
            [gdl.viewport :as viewport]))

(def ^:private -k :ctx/ui-viewport)

(defn add [ctx config]
  (assoc ctx -k (viewport/ui-viewport (:ui-viewport config))))

(extend-type gdl.application.Context
  c/UIViewport
  (ui-mouse-position [ctx]
    (viewport/mouse-position (-k ctx)))

  (ui-viewport-width [ctx]
    (:width (-k ctx)))

  (ui-viewport-height [ctx]
    (:height (-k ctx))))
