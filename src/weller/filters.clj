(ns weller.filters)

(defn aspect-added
  [name]
  (filter #(contains? (get-in % [:resource :aspect-names]) name)))

(defn or [& args]


  )
