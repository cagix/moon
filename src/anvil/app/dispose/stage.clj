(ns anvil.app.dispose.stage
  (:require [anvil.app.dispose :as dispose]
            [gdl.stage :as stage]
            [gdl.utils :refer [defn-impl]]))

(defn-impl dispose/stage []
  (stage/cleanup))
