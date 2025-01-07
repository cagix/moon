(ns gdl.context.ui
  (:require [clojure.gdx.vis-ui :as vis-ui]))

(defn dispose [_]
  (vis-ui/dispose))
