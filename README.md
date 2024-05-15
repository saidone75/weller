# weller
Weller is like Alfresco out-of-process extensions but 100% Clojure.

## Outline
- Lean and readable code
- 100% pure Clojure
- 100% test coverage

## Usage
### Create and compose filters
Filters can be composed of a single predicate:
```clojure
(filters/event? events/node-updated)
```
or a composition of predicates with `every-pred` (logical AND) or `some-fn` (logical OR):
```clojure
(every-pred (filters/event? events/node-updated) (filters/is-file?))
(some-fn (filters/node-aspect? cm/asp-titled) (filters/node-aspect? cm/asp-dublincore))
(some-fn (filters/node-aspect? cm/asp-titled) (filters/node-aspect? cm/asp-dublincore))
(every-pred (filters/event? events/node-updated) (some-fn (filters/node-aspect? cm/asp-titled) (filters/node-aspect? cm/asp-dublincore)))
```

## License
Copyright (c) 2024 Saidone

Distributed under the GNU General Public License v3.0