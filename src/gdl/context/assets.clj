(ns gdl.context.assets
  (:require [clojure.gdx :as gdx]))

(defn dispose [[_ asset-manager]]
  (gdx/dispose asset-manager))
