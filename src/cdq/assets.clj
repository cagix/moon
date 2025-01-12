(ns cdq.assets
  (:require [clojure.string]))

(def ^:private asset-type-class-map
  {:sound   com.badlogic.gdx.audio.Sound
   :texture com.badlogic.gdx.graphics.Texture})

(defn- asset-type->class [k]
  (get asset-type-class-map k))

(defn class->asset-type [class]
  (some (fn [[k v]] (when (= v class) k)) asset-type-class-map))

(defn create [_context {:keys [folder type-exts]}]
  (let [assets (for [[asset-type extensions] type-exts
                     file (map #(clojure.string/replace-first % folder "")
                               (loop [[file & remaining] (.list (.internal com.badlogic.gdx.Gdx/files folder))
                                      result []]
                                 (cond (nil? file)
                                       result

                                       (.isDirectory file)
                                       (recur (concat remaining (.list file)) result)

                                       (extensions (.extension file))
                                       (recur remaining (conj result (.path file)))

                                       :else
                                       (recur remaining result))))]
                 [file asset-type])
        manager (proxy [com.badlogic.gdx.assets.AssetManager clojure.lang.IFn] []
                  (invoke [^String path]
                    (let [^com.badlogic.gdx.assets.AssetManager this this]
                      (if (.contains this path)
                        (.get this path)
                        (throw (IllegalArgumentException. (str "Asset cannot be found: " path)))))))]
    (doseq [[file asset-type] assets]
      (.load manager ^String file (asset-type->class asset-type)))
    (.finishLoading manager)
    manager))
