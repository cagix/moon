(ns gdl.assets
  (:refer-clojure :exclude [load])
  (:import (com.badlogic.gdx.assets AssetManager)))

(defn manager
  "Assets are a collection of vectors `[file class]`.
  All assets are loaded immediately.
  Returns an `com.badlogic.gdx.assets.AssetManager` which supports `get`

  Has to be disposed."
  [assets]
  (let [manager (proxy [AssetManager clojure.lang.ILookup] []
                  (valAt [path]
                    (.get this ^String path)))]
    (doseq [[file class] assets]
      (.load manager ^String file ^Class class))
    (.finishLoading manager)
    manager))

(defn of-class
  "Returns all asset paths with the specific class."
  [manager class]
  (filter #(= (.getAssetType manager %)
              class)
          (.getAssetNames manager)))
