(ns forge.assets
  (:import (com.badlogic.gdx.assets AssetManager)))

(declare ^AssetManager manager)

(defn- asset-manager ^AssetManager []
  (proxy [AssetManager clojure.lang.IFn] []
    (invoke [^String path]
      (if (AssetManager/.contains this path)
        (AssetManager/.get this path)
        (throw (IllegalArgumentException. (str "Asset cannot be found: " path)))))))

(defn load-all [assets]
  (let [manager (asset-manager)]
    (doseq [[file class] assets]
      (.load manager ^String file ^Class class))
    (.finishLoading manager)
    (.bindRoot #'manager manager)))

(defn dispose []
  (.dispose manager))
