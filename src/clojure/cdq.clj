(in-ns 'clojure.core)

(defn sprite-batch [_context _config]
  (com.badlogic.gdx.graphics.g2d.SpriteBatch.))

(defn asset-manager
  ([_context _config]
   (asset-manager
    (let [folder "resources/"]
      (for [[asset-type extensions] {com.badlogic.gdx.audio.Sound      #{"wav"}
                                     com.badlogic.gdx.graphics.Texture #{"png" "bmp"}}
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
        [file asset-type]))))
  ([assets]
   (let [manager (proxy [com.badlogic.gdx.assets.AssetManager clojure.lang.IFn] []
                   (invoke [^String path]
                     (let [^com.badlogic.gdx.assets.AssetManager this this]
                       (if (.contains this path)
                         (.get this path)
                         (throw (IllegalArgumentException. (str "Asset cannot be found: " path)))))))]
     (doseq [[file asset-type] assets]
       (.load manager ^String file asset-type))
     (.finishLoading manager)
     manager)))
