(ns cdq.graphics.draw.line
  (:require [clojure.gdx.shape-drawer :as sd]))

(defn do! [{:keys [graphics/shape-drawer]} [sx sy] [ex ey] color]
  (sd/line! shape-drawer [sx sy] [ex ey] color))
