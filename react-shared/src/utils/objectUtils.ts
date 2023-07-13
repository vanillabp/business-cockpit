
const getObjectProperty = (object: any, path: string, defaultValue?: string): string | undefined => {
    if (!Boolean(object)) { // null or undefined
      return defaultValue;
    }
    const parts = path.split('.');
    return parts.reduce((object, key) => object?.[key], object);
};

export { getObjectProperty };
