(ns anvil.app.dispose.stage
  (:require [anvil.app.dispose :as dispose]
            [gdl.stage :as stage]))

(defn-impl dispose/stage []
  (stage/cleanup))
